(ns open-air.core
  (:require [cheshire.core :as json]
            [clojure.set :refer [difference union]]
            [clj-http.client :as http]))

(def base-url "http://playlist.cprnetwork.org/api/playlistCO?n=")
(def todays-playlist (atom nil))
(defonce existing-tracks (atom #{}))

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
  (if @todays-playlist
    @todays-playlist
    (reset! todays-playlist
            (first
             (filter
              (fn [p]
                (= (current-playlist-name)
                   (get p "name")))
              (rdio-playlists))))))

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

(defn rdio-add-track [pl-key t-key]
  (json/parse-string
   (:body
    (http/post
     rdio-base-url
     {:headers {"Authorization" token}
      :query-params {"method" "addToPlaylist"
                     "playlist" pl-key
                     "duplicateHandling" "ignore"
                     "tracks" [t-key]}}))))

(defn process-track [oa-info]
  (if-let [rdio-track (rdio-query (str (get oa-info "artist")
                                       " "
                                       (get oa-info "title")))]
    (do
      (println "Found RDIO track for " (get oa-info "title") ": " (get rdio-track "key"))
      (if-let [todays-playlist (current-playlist)]
        (do
          (println "found existing PL: " (get todays-playlist "key"))
          (println
           (rdio-add-track (get todays-playlist "key")
                           (get rdio-track "key"))))
        (do
          (println "no existing PL found, will try to create")
          (reset! todays-playlist
                  (get (rdio-create-playlist
                        (current-playlist-name)
                        (get rdio-track "key"))
                       "result"))
          (println "todays-playlist: " @todays-playlist))))))

(defn process-track-list! [tracks]
  (doseq [t tracks]
    (println "Will process track: " t)
    (process-track t)))

(defn scrape-new-tracks! []
  (let [current-tracks (into #{} (format-tracks (current-tracks)))
        new-tracks (difference current-tracks @existing-tracks)]
    (println "Have " (count @existing-tracks) " existing tracks")
    (println "Found " (count new-tracks) " new tracks")
    (println "Here they are!:::")
    (println new-tracks)
    (process-track-list! new-tracks)
    (swap! existing-tracks union new-tracks)
    (println "added new tracks; now have " (count @existing-tracks) " ext tracks")))

;; 1 - [X] find all the recent tracks
;; 2 - [X] Create a Playlist for today's date
;; 3 - [X] Need to add recent tracks to the Playlist
;;         for today
;;         - ?? How do we avoid double-adding tracks



;; approaches to diff-adding tracks

;; 1 - just tell RDIO to ignore dupes and add everything
;; 2 - keep a list of tracks seen so far and diff the list and
;;     only add new ones
;; 3 - maybe possible to use the timestamp on the json endpoint
;;     to pull only recent stuff


;; Next Steps:
;; figuring out long-running processing
;; * how to grab the current playlist once and hang onto it
;;   (atom)
;; * how to store a list of all tracks and maybe try to diff them?
;; * or tell rdio to just ignore dupes?
