(ns wikicrawl.util
  (:use wikicrawl.config)
  (:import [java.net URLEncoder])
  (:require [clojure.java.io :as io]
            [clj-http.client :as client]
            [cheshire.core :refer :all]
            [me.shenfeng.mustache :as mustache]
            [net.cgrand.enlive-html :as html]))

(def counter (atom 1))

(defn valid? [lang pagename]
  (every? nil? (map #(re-find % pagename) (lang blacklist))))

(defn xml-unescape [escape-str]
  (if (and escape-str (.startsWith escape-str "&"))
    (cond
      (= escape-str "&amp;") "&"
      (= escape-str "&gt;") ">"
      (= escape-str "&lt;") "<"
      (= escape-str "&quot;") "\""
      (= escape-str "&apos;") "'"
      (re-matches #"\&\#\d+;" escape-str)
        (String/valueOf (char (Integer/parseInt (second (re-matches #"\&\#(\d+);" escape-str)))))
      true (throw (RuntimeException. (str "Unknown xml escape sequence: " escape-str))))
    escape-str))

; please check with ns id = 14 at the page
; https://{{lang}}.wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=namespaces
(mustache/deftemplate tmpl-fn (slurp (io/resource "article.tpl")))

(defn transliter [s]
  (net.gcardone.junidecode.Junidecode/unidecode s))

(defn category? [lang page]
  (.startsWith page (str (lang category-ns-of) ":")))

(defn specials? [page]
  (.contains page ":"))

(defn to-name [page]
  (.substring page (inc (.lastIndexOf page ":"))))

(defn to-category [lang page]
  (str (lang category-ns-of) ":" page))

(defn to-yaml-name [lang page]
  (str
    (.. (to-name page)
        (replace "'" "_")
        (replace "," "_")
        (replace " " "_")
        (replace "(" "[")
        (replace ")" "]"))
    (if (category? lang page) "" ".yaml")))

(defn to-text-name [lang page]
  (str
    (.. (to-name page)
        (replace "'" "_")
        (replace "," "_")
        (replace " " "_")
        (replace "(" "[")
        (replace ")" "]"))
    (if (category? lang page) "" ".yaml")))

(defn query-subcat [lang pagename]
  (map #(get % :title)
    (get-in
      (parse-string (:body (client/get
        (str "https://" (name lang) ".wikipedia.org/w/api.php?maxlag=5&"
             "action=query&format=json&list=categorymembers&cmtype=subcat&redirects&"
             "cmtitle=" (URLEncoder/encode pagename))
        {:cookie-policy :standard :client-params {"http.useragent" "wkicrawler"}})) true)
      [:query :categorymembers])))

(defn query-article [lang pagename]
  (map #(get % :title)
    (get-in
      (parse-string
        (xml-unescape (:body (client/get
          (str "https://" (name lang) ".wikipedia.org/w/api.php?"
               "action=query&format=json&list=categorymembers&cmtype=page&cmnamespace=0&redirects&"
               "cmtitle=" (URLEncoder/encode pagename))
          {:cookie-policy :standard :client-params {"http.useragent" "wkicrawler"}}))) true)
      [:query :categorymembers])))

(defn query-langlinks [lang pagename]
  ;(Thread/sleep 300)
  (:langlinks (first (vals
    (get-in
      (parse-string (xml-unescape (:body (client/get
        (str "https://" (name lang) ".wikipedia.org/w/api.php?"
             "action=query&format=json&prop=langlinks&"
             "titles=" (URLEncoder/encode pagename))
        {:cookie-policy :standard :client-params {"http.useragent" "wkicrawler"}}))) true)
        [:query :pages])))))

(defn query-categories [lang pagename]
  ;(Thread/sleep 300)
  (map #(get % :title) (:categories (first (vals
    (get-in (parse-string (xml-unescape (:body (client/get
      (str "https://" (name lang) ".wikipedia.org/w/api.php?"
           "action=query&format=json&prop=categories&clshow=!hidden&"
           "titles=" (URLEncoder/encode pagename))
      {:cookie-policy :standard :client-params {"http.useragent" "wkicrawler"}}))) true)
      [:query :pages]))))))


(defn query-page [lang pagename]
  ;(Thread/sleep 300)
  (first (vals
    (get-in
      (parse-string (:body (client/get
        (str "https://" (name lang)
             ".wikipedia.org/w/api.php?action=parse&uselang="
             (lang lang-variant)
             "&redirects&disablepp&prop=text&format=json&wrapoutputclass=_&page="
             (URLEncoder/encode pagename))
        {:cookie-policy :standard})))
      ["parse" "text"]))))

(defn mk-langlinks [lang page]
  (sort-by #(:lang %)
    (filter #(contains? langs (keyword (:lang %)))
           (into [{:lang (name lang) :name page}]
                 (clojure.set/rename (query-langlinks lang page) {:* :name})))))

(defn mk-categories [lang page]
  (sort-by last
    (map #(hash-map :name %)
      (map to-name
        (query-categories lang page)))))

(defn gen-content [lang page tree]
  (println (str lang ":" page))
  (if (.equals (name lang) "en")
    (let [treepath (map #(hash-map :name %) tree)
          langlinks (mk-langlinks lang page)
          allcategories (map #(hash-map :lang (:lang %) :categories
                                        (mk-categories (:lang %) (:name %))) langlinks)]
      (tmpl-fn {:treepath treepath :names langlinks
                :allcategories allcategories}))))

(defn gen-page [lang pagename]
  (println (str lang ":" pagename))
  (if (.equals (name lang) "en")
    (try
      (-> (query-page lang pagename)
          (clojure.string/replace #"\n" " ")
          java.io.StringReader.
          html/html-resource
          (html/at #{[:.metadata] [:.notice] [:.toc] [:.reflist] [:.printfooter]
                     [:.noprint] [:.infobox] [:.navbox] [:.reference]
                     [:.references] [:.mw-editsection]} (fn [x] nil))
          html/texts
          first)
      (catch Throwable e (.printStackTrace e) (Thread/sleep 300)))))
