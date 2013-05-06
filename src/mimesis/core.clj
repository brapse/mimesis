(ns mimesis.core
  (:gen-class :main true)
  (:use [clojure.tools.cli :only [cli]])
  (:import [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.fs FileSystem Path]
           [org.apache.hadoop.io IOUtils LongWritable BytesWritable SequenceFile SequenceFile$Reader]))

(defn create-reader [input-path]
  (let [config (doto (Configuration.)
                     (.set "fs.default.name" "hdfs://hadoop.int.s-cloud.net"))
        fs (FileSystem/get config)]
  (SequenceFile$Reader.
    fs 
    (Path. input-path)
    config)))

(defn create-writer [output-path]
  (let [config (doto (Configuration.)
                     (.set "fs.default.name" "hdfs://hadoop.int.s-cloud.net"))
        fs (FileSystem/get config)]
  ;XXX: might want to change the compression codec
  (SequenceFile/createWriter
    fs 
    config
    (Path. output-path)
    LongWritable
    BytesWritable)))

(defn get-file-size [input-path]
  (let [config (doto (Configuration.)
                     (.set "fs.default.name" "hdfs://hadoop.int.s-cloud.net"))
        fs (FileSystem/get config)]
  (.getLen (.getFileStatus fs (Path. input-path)))))

(defn create-kv []
  (list (org.apache.hadoop.io.LongWritable.)
        (org.apache.hadoop.io.BytesWritable.)))

(defn read-tuple [reader]
  (let [key (org.apache.hadoop.io.LongWritable.)
        value (org.apache.hadoop.io.BytesWritable.)]
    (do 
      (.next reader key value)
      (list key value))))

(defn print-kv [key val]
  (println key (.toString val)))

(defn read-some [reader]
 (for [i (range 0 10)]
   (let [[key val] (create-kv)]
     (.next reader key val)
       (print-kv key (String. (.getBytes val) 0 (.getLength val))))))

(defn generate-averager []
  (let [sum (atom 0)
        n (atom 0)]
    (fn [num]
      (do
        (swap! sum + num)
        (swap! n inc)
        (/ @sum @n)))))

(defn generate-sampler [sample-size file-size]
  (let [average-size-of (generate-averager)]
    (fn [line]
      (< (rand)
         (/ sample-size
           (/ file-size
              (average-size-of line)))))))

(defn always-sample [& args] true)
(defn sample-pipe [in-file out-file sample-size uniform]
  (let [reader (create-reader in-file)
        writer (create-writer out-file)
        sample? (if uniform 
                  (generate-sampler sample-size
                                  (get-file-size in-file))
                  always-sample)]

    (do 
      (loop [cnt 0]
        (when (> sample-size cnt)
          (let [[k v] (read-tuple reader)]
            (recur (if (sample? (.getLength v))
                     (do 
                         (.append writer k v)
                         (inc cnt))
                     cnt)))))
      (IOUtils/closeStream writer))))

(defn -main [& args]
  (let [[options args banner] (cli args
                                   ["-n" "--sample-size" "Desired sample size" :parse-fn #(Integer. %) :default 10]
                                   ["-u" "--uniform" "Sample uniformly from the source file"
                                    :default false :flag true]
                                   ["-h" "--help" "Show help" :default false :flag true])]
    (when (:help options)
      (println banner)
      (System/exit 1))
    (when (< (count args) 2)
      (println "Requires an input and output file\n")
      (println banner)
      (println "\nExample: mimesis -n 10 input-file.seq output-file.seq")
      (System/exit 1))
    (let [[input-file output-file] args]
      (sample-pipe input-file
                   output-file
                   (:sample-size options)
                   (:uniform options)))))
