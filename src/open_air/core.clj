(ns open-air.core
  (:require [cheshire.core :as json]
            [clj-http.client :as http]))


(def base-url "http://playlist.cprnetwork.org/api/playlistCO?n=")

(defn current-tracks-url []
  (str base-url (System/currentTimeMillis)))

(defn current-tracks []
  (json/parse-string
   (:body
    (http/get (current-tracks-url)))))

(defn format-track [t]
  (select-keys t
               ["album" "artist" "title" "start_time" "label" "label_num"]))


(defn format-tracks [tracks]
  (map format-track tracks))

(def sample-tracks (current-tracks))
(first (format-tracks sample-tracks))
