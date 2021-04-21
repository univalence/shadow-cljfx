(ns article.ex-05

  (:require
   [cljfx.api :as fx]
   [article.ex-05.webview :as wv]
   [hiccup.page :refer [html5]]
   [shadow.cljs.devtools.api :as shadow]))

(def OUTPUT_DIR "target/ex-05")

(def build-conf
  {:build-id   :ex-05
   :output-dir OUTPUT_DIR
   :target     :browser
   :modules    {:main {:init-fn 'article.ex-05.vue/init}}})

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
                       :id      :ex-05
                       :html    MARKUP
                       :handler (fn [message] (println "received: " message))
                       :on-load (fn [_ _] (println "web view loaded."))}}}))

(comment
 (wv/send! :ex-05 "hello")
 (println MARKUP))
