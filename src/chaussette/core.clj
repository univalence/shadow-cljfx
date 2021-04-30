(ns chaussette.core
  (:require [cljfx.api :as fx]
            [cljfx.ext.web-view :as web-view]
            [chaussette.server :as server]))


(def VIEW_URL "http://localhost:3000/view1/index.html")

(def web-view
  {:fx/type web-view/with-engine-props
   :props   {:url VIEW_URL}
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

(defonce server
  (server/start! 3000))



(comment

 (server/stop! server)
 (renderer {:fx/type root
            :title "shadow-cljfx"
            :status "ok"})

 (server/send! :chaussette.view1 "Coucou")

 (require '[shadow.cljs.devtools.api :as shad])
 (shad/release :c1))