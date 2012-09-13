lg
==

Logging library for Clojure

## Usage

Example

```clj
(ns lg-example
  (:require lg))

(defn -main
  [& args]
  (lg/add-channel! (lg/stderr-channel))
  (lg/set-level! 'lg-example lg/INFO)
  (lg/info "Hello %s!" "there"))
```

## License

Copyright 2012 Brandon Bickford.  Licensed under the Apache License 2.0.  Please refer to LICENSE.
