(ns open-air.core
  (:require [cheshire.core :as json]
            [clj-http.client :as http]))

(def url
  (java.net.URL.
    "http://www.cpr.org/openair/playlist"))

(println "url is: " url)

(def page-contents
  (html/html-resource url))

(println page-contents)

(html/select page-contents [:div#main])
(println (count (html/select page-contents [:.list-item])))

(count (str (System/currentTimeMillis)))
(count (str "1445347939413"))
