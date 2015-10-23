(ns lastfm-widget-reagent.core
    (:require [reagent.core :as reagent]))

(defn home []
  [:div
   [:h "HellWorld!"]])

(defn ^:export main []
  (reagent/render [home]
                  (.getElementById js/document "app")))

(defn on-js-reload []
  )
