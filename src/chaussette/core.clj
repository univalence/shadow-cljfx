(ns chaussette.core
  (:require [cljfx.api :as fx]
            [cljfx.ext.web-view :as web-view]
            [chaussette.server :as server]))


(def VIEW_URL "http://localhost:3000/view1/index.html")

(defn root [_]
      {:fx/type :stage
       :showing true
       :scene   {:fx/type :scene
                 :root {:fx/type web-view/with-engine-props
                        :props   {:url VIEW_URL}
                        :desc    {:fx/type :web-view}}}})


(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc #'root)))

(defonce server
  (server/start! 3000))



(comment

 (renderer)

 (server/send! :chaussette.view1
               (rand-nth [:ping :pong]))

 (server/stop! server)

 (require '[shadow.cljs.devtools.api :as shad])
 (shad/release :view1))