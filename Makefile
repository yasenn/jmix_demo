.PHONY: run build clean shell

run:
	nix-shell --run "cd poc-service-request && gradle bootRun"

build:
	nix-shell --run "cd poc-service-request && gradle build"

clean:
	nix-shell --run "cd poc-service-request && gradle clean"

shell:
	nix-shell
