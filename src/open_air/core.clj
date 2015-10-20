(ns open-air.core
  (:require [cheshire.core :as json]
            [clj-http.client :as http]))


(def base-url "http://playlist.cprnetwork.org/api/playlistCO?n=")

(defn current-tracks-url []
  (str base-url (System/currentTimeMillis)))

(current-tracks-url)
