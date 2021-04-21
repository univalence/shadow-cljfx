(ns article.ex-05
  (:require
   [cljfx.api :as fx]
   [article.ex-05.webview :as ww]
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

(declare send!)

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :scene   {:fx/type :scene
             :root    {:fx/type ww/engine-ext
                       :desc    {:fx/type :web-view}
                       :props   {:html     MARKUP
                                 :bridge   ww/bridge_log
                                 :on-error (fn [e] (println "error: " e))
                                 :on-load  (fn [engine _]
                                             (defn send! [data]
                                               (fx/on-fx-thread
                                                (println "sending " data)
                                                (.executeScript engine (str "fromJava(\"" data "\")"))))
                                             (println "web view loaded."))}}}}))

(comment
 (send! "hello"))
