(disable-warning
 {:linter :redefd-vars
  :if-inside-macroexpansion-of #{'mount.core/defstate}
  :within-depth 2
  :reason "Mount's defstate macro generates two defs, which trips the Eastwood redefd-vars linter."})
