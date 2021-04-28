(ns article.ex-05.step2
  (:require
   [cljfx.api :as fx]
   [cljfx.prop :as prop]
   [cljfx.mutator :as mutator]
   [cljfx.component :as comp]
   [cljfx.lifecycle :as lc])
  (:import (javafx.scene.web WebView)))

(defprotocol ISend
  (send [this message]))

(defn sender
  "turn the given function into an instance of ISend"
  [f]
  (reify ISend
    (send [_ message] (f message))))

(defn wrap-instance [lifecycle f]
  (reify lc/Lifecycle
    (create [_ desc opts]
      (let [this (lc/create lifecycle desc opts)]
        (f (comp/instance this))
        this))
    ;; boilerplate
    (advance [_ component desc opts]
      (lc/advance lifecycle component desc opts))
    (delete [_ component opts]
      (lc/delete lifecycle component opts))))

(def engine-ext

  (fx/make-ext-with-props

   {:html    (prop/make
              (mutator/setter
               #(.loadContent (.getEngine ^WebView %1) %2 "text/html"))
              lc/scalar)

    :handler (prop/make
              (mutator/setter
               (fn [this f]
                 (let [engine (.getEngine ^WebView this)
                       window (.executeScript engine "window")]
                   (.setMember window "app" (sender f)))))
              lc/scalar)
    }))

(def html-content
  "<!DOCTYPE html><html>
    <body>
      <div id=\"app\"> Hello </h1>
      <button onclick=\"app.send('anyone here ?')\">anyone here ?</button>
      <script>
        var webView = {send: function(message){
          document.getElementById('app').innerHTML = message;
          }
        }
      </script>
    </body>
   </html>")

(def engine (atom nil))

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :y       -1000 :x 1000
   :scene   {:fx/type :scene
             :root    {:fx/type (wrap-instance engine-ext #(reset! engine (.getEngine %)))
                       :desc    {:fx/type :web-view}
                       :props   {:html        html-content
                                 :handler     (fn [data] (println "Client sent: " data))}}}}))

(defn send! [message]
  (fx/on-fx-thread
   (.executeScript @engine (str "webView.send(" (pr-str message) ")"))))


(comment
 (send! "Bonjour"))