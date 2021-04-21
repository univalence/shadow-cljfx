(ns app.core
  (:require [cljfx.api :as fx]
            [cljfx.ext.web-view :as web-view]
            [app.server]))

(def web-view
  {:fx/type web-view/with-engine-props
   :props   {:content (slurp "target/index.html")}
   :desc    {:fx/type :web-view}})

(defn root [{:keys [title status]}]
      {:fx/type :stage
       :x       1000 :y -1000
       :showing true
       :title   (str title)
       :scene   {:fx/type :scene
                 :root
                          {:fx/type  :v-box
                           :children [web-view
                                      {:fx/type :label
                                       :text    (str status)}]}}})

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc #'root)))

(renderer {:fx/type root
           :title "shadow-cljfx"
           :status "ok"})