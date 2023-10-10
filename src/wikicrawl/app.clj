(ns wikicrawl.app
  (:use [wikicrawl.util])
  (:require [wikicrawl.cat :as cat]
            [wikicrawl.text :as txt])
  (:gen-class))

(defn -main []
  (cat/start-crawl-cat :en)
  (.start (Thread. (fn [] (cat/make-crawl-worker)))))