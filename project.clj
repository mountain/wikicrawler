(defproject wikicrawler "0.1.0"

  :description "A crawler to achieve the category structure of wikipedia"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"]
                 [circleci/clj-yaml "0.6.0"]
                 [enlive "1.1.6"]
                 [me.shenfeng/mustache "1.1"]
                 [cheshire "5.11.0"]
                 [com.taoensso/carmine "3.2.0"]
                 [dnsjava/dnsjava "3.5.2"]
                 ]

  :source-paths ["src"]
  :java-source-paths ["java"]
  :resource-paths ["templates"]
  :target-path "target/%s/"
  :filespecs [{:type :path :path "templates/article.tpl"}]
  :auto-clean false

  :aot :all
  :main wikicrawl.app)


