(ns wikicrawl.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.carmine.message-queue :as car-mq]))

(def redis-conn {:pool {} :spec {:uri "redis://192.168.0.14:6379"}})

(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

(defn enqueue-txt [kind lang page tree path depth progress]
  (wcar* (car-mq/enqueue "wikicrawl:txt:queue" [kind lang page tree path depth progress])))

(defn enqueue-cat [kind lang page tree path depth progress]
  (wcar* (car-mq/enqueue "wikicrawl:cat:queue" [kind lang page tree path depth progress])))

(defn mk-qtxt-worker [handler]
  (car-mq/worker redis-conn "wikicrawl:txt:queue" handler))

(defn mk-qcat-worker [handler]
  (car-mq/worker redis-conn "wikicrawl:cat:queue" handler))

(defn not-analyzing-yet [lang page]
  (zero? (wcar* (car/exists (str "wikicrawl:analyzed:" lang ":" page)))))

(defn record-analyzing [lang page]
  (wcar*
    (car/set (str "wikicrawl:analyzed:" lang ":" page) java.lang.Boolean/FALSE)))

(defn check-analyzing [lang page]
  (and (not (zero? (wcar* (car/exists (str "wikicrawl:analyzed:" lang ":" page)))))
    (false? (wcar* (car/get (str "wikicrawl:analyzed:" lang ":" page))))))

(defn record-analyzed [lang page]
  (wcar*
    (car/set (str "wikicrawl:analyzed:" lang ":" page) java.lang.Boolean/TRUE)))

(defn check-analyzed [lang page]
  (and (not (zero? (wcar* (car/exists (str "wikicrawl:analyzed:" lang ":" page)))))
       (true? (wcar* (car/get (str "wikicrawl:analyzed:" lang ":" page))))))

(defn not-downloading-yet [lang page]
  (not (zero? (wcar* (car/exists (str "wikicrawl:downloaded:" lang ":" page))))))

(defn record-downloading [lang page]
  (wcar*
    (car/set (str "wikicrawl:downloaded:" lang ":" page) java.lang.Boolean/FALSE)))

(defn check-downloading [lang page]
  (and (not (zero? (wcar* (car/exists (str "wikicrawl:downloaded:" lang ":" page)))))
       (false? (wcar* (car/get (str "wikicrawl:downloaded:" lang ":" page))))))

(defn record-downloaded [lang page]
  (wcar*
    (car/set (str "wikicrawl:downloaded:" lang ":" page) java.lang.Boolean/TRUE)))

(defn check-downloaded [lang page]
  (and (not (zero? (wcar* (car/exists (str "wikicrawl:downloaded:" lang ":" page)))))
       (true? (wcar* (car/get (str "wikicrawl:downloaded:" lang ":" page))))))

