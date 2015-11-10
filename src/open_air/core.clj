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
               ["album" "artist" "title"
                "start_time" "label" "label_num"]))

(defn format-tracks [tracks]
  (map format-track tracks))

(def rdio-base-url "https://services.rdio.com/api/1/")

(def token
  (str "Bearer " (System/getenv "RDIO_TOKEN")))

(defn rdio-query [query]
  (first
   (get-in
    (json/parse-string
     (:body
      (http/post
       (str rdio-base-url "search")
       {:query-params {"query" query
                       "method" "search"
                       "types" "Track"}
        :throw-exceptions false
        :headers {"Authorization" token}})))
    ["result" "results"])))

(defn rdio-playlists []
  (get-in
   (json/parse-string
    (:body
     (http/post
      rdio-base-url
      {:headers {"Authorization" token}
       :query-params {"method" "getPlaylists"}})))
   ["result" "owned"]))

(defn current-playlist-name []
  (str
   "Open Air "
   (.format (java.text.SimpleDateFormat. "MMMM d") (java.util.Date.))))

(defn current-playlist []
  (first
   (filter
   (fn [p]
     (= (current-playlist-name)
        (get p "name")))
   (rdio-playlists))))

(defn rdio-create-playlist [name tracks]
  (json/parse-string
   (:body
    (http/post
     rdio-base-url
     {:headers {"Authorization" token}
      :query-params {"method" "createPlaylist"
                     "name" name
                     "description" "Open Air tracks"
                     "tracks" tracks}}))))

(defn todays-playlist []
  (if-let [pl (current-playlist)]
    pl
    "To do - create the playlist"))


(def some-tracks (format-tracks (current-tracks)))

(let [t {"title" "Monster Mash" "artist" "Bobby “Boris” Pickett"}
      res (rdio-query (str (get t "artist") " " (get t "title")))]
  (if res
    (rdio-create-playlist
     (current-playlist-name)
     [(get res "key")])))

(defn rdio-add-track [pl-key t-key]
  (json/parse-string
   (:body
    (http/post
     rdio-base-url
     {:headers {"Authorization" token}
      :query-params {"method" "addToPlaylist"
                     "playlist" pl-key
                     "tracks" [t-key]}}))))

(defn process-track [oa-info]
  (if-let [rdio-track (rdio-query (str (get oa-info "artist")
                                       " "
                                       (get oa-info "title")))]
    (if-let [todays-playlist (current-playlist)]
      (rdio-add-track (get todays-playlist "key")
                      (get rdio-track "key"))
      (rdio-create-playlist
       (current-playlist-name)
       (get rdio-track "key")))))

;; 1 - find all the recent tracks
;; 2 - Create a Playlist for today's date
;; 3 - Need to add recent tracks to the Playlist
;;     for today
;;     - ?? How do we avoid double-adding tracks
