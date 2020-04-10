setup-dev:
	@brew install borkdude/brew/clj-kondo


.clj-kondo:
	@mkdir -p .clj-kondo
	@-clj-kondo --lint "$(shell lein classpath)"


.PHONY: lint
lint: .clj-kondo
	@clj-kondo --lint src
	@lein do check, lint


.PHONY: test
test:
	@lein test
