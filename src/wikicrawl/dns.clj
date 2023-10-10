(ns wikicrawl.dns
  (:import (wikicrawl DohResolver)))

(def resolver (DohResolver. "1.1.1.1"))

