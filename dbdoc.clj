#!/usr/bin/env bb

;; Convert a simple ORG file into SQL COMMENTs for documenting tables
;; and their columns.
;;
;; Environmnent variables for controlling behavior:
;; - DBDOC_SQL
;; - DBDOC_ORG
;; - DBDOC_HTML

(require '[babashka.fs :as fs]
         '[clojure.java.shell :refer [sh]]
         '[clojure.java.io :as io]
         '[clojure.string :as str]) ; not really needed since babashka
(import 'java.time.format.DateTimeFormatter
        'java.time.LocalDateTime)

;; https://stackoverflow.com/a/62970721/326516
(def timestamp
  "Migration-compatible timestamp (as seen in Migratus and elsewhere);
  eg, 20210205111716"
  (.format (LocalDateTime/now)
           (DateTimeFormatter/ofPattern "yyyyMMddHHmmss")))

;; Default files for user to configure
(def sql-file
  "Output SQL (migration) file containing timestamp"
  (or (System/getenv "DBDOC_SQL")
      (format "resources/migrations/%s-dbdoc.up.sql" timestamp)))
(def mig-dir
  "Migration directory, for deletion of old migs"
  (.getParent (io/file sql-file)))
(def org-file
  "Input file containing table/col descriptions"
  (let [f (or (System/getenv "DBDOC_ORG")  "docs/dbdoc.org")]
    (if-not (fs/exists? f)
      (do (println "ERROR: input org-file does not exist")
          (System/exit 1))
      f)))
(def html-file
  "Output HTML file for publishing"
  (or (System/getenv "DBDOC_HTML") "docs/dbdoc.html"))


;;; Translation

(defn check-dir
  "Check for existence of directory for file `f`."
  [f]
  ;; (println "Checking for existence of directory:" f)
  (let [dir (.getParent (io/file f))]
    (when-not (.isDirectory (io/file dir))
      (println "ERROR:" dir "directory does not exist.")
      (System/exit 1))))

(defn process-col
  "Convert a column description into a SQL COMMENT.
  Construct a dotted table.column name, replace hyphens w/ underscores."
  [tab col]
  (let [colname (-> (str tab "." (str/replace col #"(?s)^- (.*) ::.*" "$1"))
                    (str/replace  #"-" "_"))
        text    (-> (str/join " " (rest (str/split col #"\n")))
                    str/trim
                    (str/replace  #"(?s)\s+" " ")
                    (str/replace  #"(?s)'" "’"))
        cmt     (format "COMMENT ON COLUMN %s IS '%s';" colname text)]
    cmt))

(defn process-section
  "Convert a section of table descriptions and combine with columns."
  [sec]
  (let [[tabname
         tabdesc
         & cols] (str/split sec #"\n\n")
        tabname  (-> tabname
                     (str/replace #"(?s)\n" " ")
                     (str/replace #"-" "_"))
        tabdesc  (-> tabdesc
                     (str/replace #"(?s)\n" " ")
                     (str/replace #"(?s)'" "’")
                     str/trim)
        cmt      (format "COMMENT ON TABLE %s IS '%s';" tabname tabdesc)]
    (cons cmt (mapv #(process-col tabname %) cols))))

(defn move-old-migration
  "Rename older migration file(s).
  Assume was successful in creating a new mig."
  []
  (when-let [old (first (fs/glob mig-dir "*-dbdoc.up.sql" {:max-depth 1}))]
    (if (= 0 (:exit (sh "git" "ls-files" "--error-unmatch" (.toString old))))
      (do (println "Moving old:" (.toString old))
          (sh "git" "mv" (.toString old) sql-file))
      (println "WARNING: Detected old dbdoc file not in git:" (.toString old)))))

(defn print-comments
  "Write all SQL comments to `sql-file`."
  [title comments]
  (move-old-migration)
  (binding [*out* (io/writer sql-file)]
    (println title)
    (println "-- DO NOT EDIT THIS FILE; SEE dbdoc.org FILE")
    ;; Could wrap in a BEGIN/COMMIT transaction here, but some migrators
    ;; do this automatically
    (doseq [c comments]
      (println)
      (doseq [i c]
        (println i)))))


;;; Main

(check-dir sql-file)

;; To test, set DBDOC_ORG=example.org
(println "Reading ORG descriptions input file:" org-file)
(def org (slurp org-file))
(def title
  (str/replace (first (str/split org #"\n\n+"))
               #"#\+Title: " "-- "))

(def sections (rest (str/split org #"(?m)^\* ")))

(def comments (mapv process-section sections))

(println "Writing SQL comments output file:" sql-file)
(print-comments title comments)
