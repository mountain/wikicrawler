(ns wikicrawl.text
  (:use [wikicrawl.config]
        [wikicrawl.util]
        [wikicrawl.redis])
  (:require [clj-yaml.core :as yaml]))

(defn crawl-file [lang page tree path depth progress]
  (try
    (let [yamlname (to-yaml-name lang page)
          filename (str path "/" yamlname)
          pagename (lang (apply merge (:Names (yaml/parse-string (slurp (transliter filename))))))
          newname (transliter (str (.substring filename 0 (inc (.lastIndexOf filename "."))) "txt"))]
      (if pagename
        (do
          (with-open [newfile (java.io.FileWriter. newname)]
            (Thread/sleep (* (+ 40 (rand-int 30)) @counter))
            (if-let [text (gen-page lang pagename)]
              (do
                (println "=>" newname)
                (.write newfile text)))))))
    (catch Throwable e (.printStackTrace e))))
