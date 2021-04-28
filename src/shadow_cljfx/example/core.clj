(ns shadow-cljfx.example.core
  (:require
   [cljfx.api :as fx]
   [shadow-cljfx.web-view :as wv]
   [hiccup.page :refer [html5]]
   [shadow.cljs.devtools.api :as shadow]))

(def OUTPUT_DIR "target/shadow_cljfx/example")
(def ID :shadow-cljfx.example)

(def build-conf
  {:build-id   ID
   :output-dir OUTPUT_DIR
   :web-view   true
   :target     :browser
   :modules    {:main {:init-fn 'shadow-cljfx.example.vue/init}}})

(def COMPILED_JS
  (shadow/with-runtime
   (shadow/release* build-conf {})
   (slurp (str OUTPUT_DIR "/main.js"))))

(def MARKUP
  (html5
   [:head
    [:meta {:charset "utf-8"}]]
   [:body
    [:div#app]
    [:script COMPILED_JS]]))

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :x       1000 :y -1000
   :scene   {:fx/type :scene
             :root    {:fx/type wv/web-view
                       :id      ID
                       :html    MARKUP
                       :handler (fn [message] (println "received: " message))
                       :on-load (fn [_ _] (println "web view loaded."))}}}))

(comment
 (wv/send! ID "hello")
 (println MARKUP))
