(ns lg
  "Simple logging module for Clojure

  org.commons.util.NIH4J
  "
  (:import [java.text SimpleDateFormat]))

(def channels (atom []))
(def ERROR 0)
(def INFO 100)
(def WARN 200)
(def DEBUG 300)

(def level-names {ERROR "ERROR"
                  WARN "WARN"
                  DEBUG "DEBUG"
                  INFO "INFO"})

(def default-date-format (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss"))

(def ns-to-level
  "A mapping of namespace to level"
  (atom {}))

(def default-format [:date "\t" :ns "\t" :filename ":" :line "\t" :message "\n"])

(defn set-level!
  "Set the level of a module"
  [namespace-symbol level]
  (swap! ns-to-level assoc namespace-symbol level))

(defn- log-to-writer
  [^java.io.Writer writer evt fmt-keys date-fmt]
  (let [throwable (:throwable evt)
        ^StackTraceElement st (when throwable (first (.getStackTrace #^Throwable throwable)))
        line-number (or (when st (.getLineNumber st) 0))
        filename (or (when st (.getFileName st)) "")]
    (doseq [fmt-key fmt-keys]
      (cond
        (= fmt-key :date) (.write writer (.format #^SimpleDateFormat date-fmt (:date evt)))
        (= fmt-key :ns) (.write writer #^String (name (:ns evt)))
        (= fmt-key :filename) (.write writer #^String filename)
        (= fmt-key :line) (.write writer #^String (format "%d" line-number))
        (= fmt-key :level) (.write writer #^String (get level-names (:level evt)))
        (= fmt-key :message) (.write writer (apply format (:format evt) (:format-vals evt)))
        (string? fmt-key) (.write writer #^String fmt-key)
        :else (throw (Exception. (format "unexpected: %s" fmt-key)))))
    (.flush writer)))

(defn- writer->channel
  [writer & opts]
  (let [{:keys [fmt date-fmt]
         :or {fmt default-format
              date-fmt default-date-format}} (apply hash-map opts)]
    (fn [evt] (log-to-writer writer evt fmt date-fmt))))

(defn stdout-channel
  "Get a channel for writing log events to standard output"
  [& opts]
  (apply writer->channel *out* opts))

(defn stderr-channel
  "Get a channel for writing log events to standard error"
  [& opts]
  (apply writer->channel *err* opts))

(defn file-channel
  "Get a channel for writing log events to a file"
  [^String path & opts]
  (apply writer->channel (clojure.java.io/writer path) opts))

(defn add-channel!
  "Add a logging channel"
  [chan]
  (swap! channels conj chan))

(defn reset-channels!
  "Empty the list of channels"
  []
  (reset! channels []))

(defn dispatch-event
  [event]
  (doseq [ch @lg/channels]
    (ch event)))

(defmacro event
  "Generate an event"
  [level fmt & fmt-vals]
  `(let [level# ~level
         ns# (ns-name ~*ns*)
         ns-level# (get @lg/ns-to-level ns# lg/ERROR)]
     (when (<= level# ns-level#)
       (let [evt# {:format ~fmt
                   :format-vals (list ~@fmt-vals)
                   :level level#
                   :throwable (Throwable. )
                   :date (java.util.Date.)
                   :ns ns#}]
         ; Move the [doseq] into a separate function so that logging macros can
         ; be called inside of try/finally blocks
         (lg/dispatch-event evt#)))))

(defmacro info
  "Report an info event"
  [fmt & bindings]
  `(lg/event lg/INFO ~fmt ~@bindings))

(defmacro debug
  "Report a debug event"
  [fmt & bindings]
  `(lg/event lg/DEBUG ~fmt ~@bindings))

(defmacro warn
  "Report a warning event"
  [fmt & bindings]
  `(lg/event lg/WARN ~fmt ~@bindings))

(defmacro error
  "Report an error event"
  [fmt & bindings]
  `(lg/event lg/ERROR ~fmt ~@bindings))

