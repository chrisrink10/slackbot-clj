setup-dev:
	@brew install borkdude/brew/clj-kondo


.clj-condo:
	@mkdir -p .clj-kondo
	@-clj-kondo --lint "$(shell lein classpath)"


.PHONY: lint
lint: .clj-condo
	@clj-kondo --lint src
	@lein do check, lint


.PHONY: test
test:
	@lein test
