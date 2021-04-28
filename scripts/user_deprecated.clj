(ns user_deprecated
  (:use backtick)
  (:require
   [clojure.pprint :as pp :refer [pprint]]
   [clojure.string :as str]
   [clojure.walk :as walk]

   [hiccup.page :refer [html5]]
   [me.raynes.fs :as fs]
   [zprint.core :as zp :refer [zprint czprint]]))

(defn index-page [{:keys [body scripts stylesheets]}]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:style#_stylefy-constant-styles_]
    [:style#_stylefy-styles_]
    (map (fn [path] [:link {:rel "stylesheet" :href path}])
         stylesheets)]
   [:body
    body
    (map (fn [path] [:script {:src path}]) scripts)]))

(index-page {:body        [:div#app]
             :stylesheets ["styles.css"]
             :scripts     ["c1/main.js" "foo.js"]})

(defn shadow-cljs_spit-index-page [build-id]
  (let [conf (read-string (slurp "shadow-cljs.edn"))]))

(def sc (read-string (slurp "shadow-cljs.edn")))

(let [build-id :c1
      output-dir (get-in sc [:builds build-id :output-dir])
      manifest (read-string (slurp (str output-dir "/manifest.edn")))
      main-build (first (filter (fn [{:keys [name]}] (= :main name)) manifest))]
  (str output-dir "/" (:output-name main-build)))

(defn shadow-build-conf [id main]
  {:output-dir (str "target/" (name id))
   :asset-path "."
   :target     :browser
   :modules    {:main {:init-fn main}}})

(defn create-component [id main]
  (let [ns (namespace main)]
    ))

(defn dashes->underscores [s]
  (str/replace s #"-" "_"))

(defn ns->path [ns]
  (-> (name ns)
      (str/replace #"\." "/")
      (dashes->underscores)))

(ns->path "foo.bar-qux")

(defn create-module [ns]
  (let [path-suffix (ns->path ns)
        src-path (str "src/" path-suffix)
        target-path (str "target/" path-suffix)
        shadow-conf (assoc-in (read-string (slurp "shadow-cljs.edn"))
                              [:builds (keyword (str ns))]
                              (shadow-build-conf path-suffix (symbol (str ns ".core/main"))))]
    ;; sources
    (fs/mkdirs src-path)
    (spit (str src-path "/core.cljs")
          (str/join "\n\n"
                    [(list 'ns (symbol (str ns ".core")) (list :require '[reagent.dom :as dom]))
                     (list 'defn 'main [] (list 'dom/render [:div "Hello"] (list 'js/document.getElementById "app")))]))
    ;; target
    (fs/mkdirs target-path)
    (spit (str target-path "/index.html")
          (index-page {:scripts ["main.js"]
                       :body    [:div#app]
                       :title   ns}))

    ;; shadow-conf
    (spit "shadow-cljs.edn"
          (with-out-str (pprint shadow-conf)))))

(create-module 'components.one)

#_(read-string (slurp "scripts/template.edn"))

(defn core-src_simple [ns]
  (template
   [(ns ~ns
        (:require
         [reagent.core :as r]
         [reagent.dom :as dom]))

    (defn render {:dev/after-load true} []
      (dom/render [:div "Hi"] (js/document.getElementById "app")))

    (defn main {:export true} []
      (render))]))

(comment
 (spit "test.cljs" (apply str (interpose "\n\n" (core-src 'foo.bar)))))



(defn core-src_ws [ns port]
  (template
   [(ns ~ns
        (:require [reagent.core :as r]
         [reagent.dom :as rd]
         [cljs.core.async :as a :refer [<! >!]]
         [haslett.client :as ws]
         [haslett.format :as fmt]
         [cljs.reader :as reader])
        (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

    (def WEBSOCKET ~(str "ws://localhost:" port "/"))

    (def ID ~(keyword (str ns)))

    (def state (r/atom {}))

    (def ws-conn (atom nil))

    (defn init-websocket! [id handle]
      (letfn [(listen []
                (go-loop []
                         (if-let [source (:source @ws-conn)]
                           (do (handle (reader/read-string (<! source))) (recur))
                           (println "something went wrong establishing websocket connection"))))]
        (go
         ;; connection
         (reset! ws-conn (<! (ws/connect (str WEBSOCKET "talk"))))
         ;; give the connection id to server
         (>! (:sink @ws-conn) [ID id])
         ;; listening to incoming messages
         (listen))))

    (defn render {:dev/after-load true} []
      (rd/render [:h1 "Hello shadow-cljfx !"
                  [:button {:on-click (fn [] (go (>! (:sink @ws-conn) "hello from client")))}
                   "say hello to server"]]
                 (.getElementById js/document "app")))

    (defn main {:export true} [& [id]]
      (init-websocket! id (fn [message] (println "ws receives: " message)))
      (render))]))

#_(mapv zprint (core-src_ws 'foo.bar 3000))

(defn pretty-file-str [exprs]
  (apply str (interpose "\n\n" (map zp/zprint-str exprs))))

(defn spit-safe [path str]
  (if (fs/exists? path)
    (throw (Exception. (str "the file " path " already exists")))
    (spit path str)))

#_(spit "scripts/test.cljs"
      (pretty-file-str (core-src_ws 'foo.bar 3000)))

#_(zp/zprint-file-str)

#_(zp/czprint-fn clojure.core/get)

(defn cljp [])