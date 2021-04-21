(ns article.ex-03
  (:require
   [cljfx.api :as fx]
   [cljfx.ext.web-view :as fx.ext.web-view]
   [hiccup.page :refer [html5]]
   [shadow.cljs.devtools.api :as shadow]))

(def OUTPUT_DIR "target/ex-03")

(def build-conf
  {:build-id   :ex-03
   :output-dir OUTPUT_DIR
   :target     :browser
   :modules    {:main {:init-fn 'article.ex-03.vue/init}}})

(defonce COMPILED_JS
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
   :scene   {:fx/type :scene
             :root    {:fx/type fx.ext.web-view/with-engine-props
                       :desc    {:fx/type :web-view}
                       :props   {:content MARKUP}}}}))



