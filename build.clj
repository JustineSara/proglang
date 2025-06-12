(ns build
    (:require [clojure.tools.build.api :as b]))

(defn clean
    [_]
      (b/delete {:path "target"}))

(defn uberjar
  [_]
  (let [basis (b/create-basis {:project "deps.edn"})]
    (clean nil)
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir "target"})
    (b/compile-clj {:basis basis
                    :ns-compile '[main]
                    :class-dir "target/classes"})
    (b/uber {:class-dir "target/classes"
             :uber-file "target/proglang.jar"
             :basis basis
             :main 'main})))
