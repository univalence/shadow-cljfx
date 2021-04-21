(ns shadow
  (:require
   [shadow.cljs.devtools.api :as shadow]
   [shadow.cljs.devtools.server :as shadow-server]
   [shadow.cljs.devtools.config :as conf]))

(def build-conf
  '{:build-id   :c1
    :output-dir "target/c1",
    :asset-path "..",
    :target     :browser,
    :modules    {:main {:init-fn app.c1/init}}})

(comment
 (shadow-server/start!)
 (shadow/compile* build-conf {})
 nil)