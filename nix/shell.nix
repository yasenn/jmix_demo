{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = with pkgs; [
    jdk17
    gradle
    curl
    unzip
  ];

  shellHook = ''
    export JAVA_HOME="${pkgs.jdk17}/lib/openjdk"
    export PATH="$JAVA_HOME/bin:$PATH"
  '';
}
