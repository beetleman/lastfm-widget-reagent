(ns lastfm-widget-reagent.core
  (:require [reagent.core :as reagent]
            [cljs.core.async :refer [chan close!]]
            [ajax.core :refer [GET POST]]))

(def ^:private api-url "//ws.audioscrobbler.com/2.0/")
(def ^:private api-key "7125d4e86e20b283e109669693c4465d")
(def ^:private api-format "json")
(def ^:private default-album-cover-url "img/cover.png")


(defn GET-params
  ([method]
   (GET-params method {}))
  ([method params]
   (merge {:api_key api-key :method method :format api-format} params)))


(defn make-handler
  ([dest-key dest] (make-handler dest-key dest []))
  ([dest-key dest get-in-path]
   (fn [response]
     (let [response-error (if (:error response) response nil)
           response (get-in response get-in-path)]
       (swap! dest assoc :error-api response-error)
       (if response
         (swap! dest assoc dest-key response))))))


(defn getRecentTracks
  ([user handler error-handler]
   (getRecentTracks user handler error-handler 50))
  ([user handler error-handler limit]
   (GET api-url
        :params (GET-params "user.getRecentTracks"
                            {:user user :limit limit})
        :response-format (keyword api-format)
        :keywords? (= api-format "json")
        :handler handler
        :error-handler error-handler)))


(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))


(defn plaing-now? [track]
  (get-in track [(keyword "@attr") :nowplaying]))


(defn get-title [{:keys [name url]}]
  [:a {:class "title"
       :href url}
   name])

(defn get-album [{:keys [album]}]
  [:div {:class "album"}
   (:#text album)])

(defn get-artist [{:keys [artist]}]
  [:div {:class "artist"}
   (:#text artist)])


(defn get-album-cover-url [{:keys [image]} size]
  (let [url (some (fn [x]
                    (if (= (:size x) size) (:#text x)))
                  image)]
    (if (-> url count zero?)
      default-album-cover-url
      url)))

(defn get-album-cover [{:keys [album] :as track}]
  [:img {:class "cover"
         :src (get-album-cover-url track "medium")}])

(defn get-error-message [error]
  [:div {:class "error"}
   (str "Error: " error)])

(defn track-view [track]
  (into [:li {:class (if (plaing-now? track)
                       "track plaing-now"
                       "track")}]
        (map #(% track)
             [get-album-cover get-title get-artist get-album])))

(def update-tracks
  ([tracks] (update-tracks tracks 5000))
  ([tracks time]
  (go
    (loop []
      (getRecentTracks
       user_name
       (make-handler :tracks tracks [:recenttracks :track])
       (make-handler :error tracks)
       track_number)
      (<! (timeout time))
      (recur)))))

(defn create-lastfm-widget-view [user_name track_number app-state]
  (fn [tracks]
      (render-state [this state]
        (cond
          (:error-api data)
          (get-error-message (get-in data [:error-api :message]))
          (:error data)
          (get-error-message "Connection problem")
          :else
          (apply dom/ul #js {:className "tracks"}
                 (om/build-all track-view (:tracks data)))))))

(defn home []
  [:div
   [:h "HellWorld!"]])

(defn ^:export main []
  (reagent/render [home]
                  (.getElementById js/document "app")))

(defn on-js-reload []
  )
