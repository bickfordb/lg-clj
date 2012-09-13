(ns lg-example
  (:require lg))

(defn -main
  [& args]
  (lg/add-channel! (lg/stderr-channel))
  (lg/set-level! 'lg-example lg/INFO)
  (lg/info "Hello %s!" "there"))

