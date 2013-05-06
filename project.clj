(defproject mimesis "0.1.0-SNAPSHOT"
  :description "A tool for sampling HDFS sequence files"
  :url "http://github.com/brapse/mimesis"
  :license {:name "BSD 2-Clause"
            :url "https://github.com/brapse/tsinkf/blob/master/LICENSE "}
  :plugins [[lein-swank "1.4.5"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.apache.hadoop/hadoop-core "0.20.2-cdh3u3"]
                 [org.clojure/tools.cli "0.2.2"]]
  :main mimesis.core
    :repositories {
        "cloudera-releases" "https://repository.cloudera.com/content/repositories/releases/"
        "conjars"           "http://conjars.org/repo/" })
