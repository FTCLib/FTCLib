FROM gitpod/workspace-full-vnc
                    
USER gitpod

# Install custom tools, runtime, etc. using apt-get
# For example, the command below would install "bastet" - a command line tetris clone:
#
# RUN sudo apt-get -q update && #     sudo apt-get install -yq bastet && #     sudo rm -rf /var/lib/apt/lists/*
#
# More information: https://www.gitpod.io/docs/42_config_docker/

USER root

RUN bash -c "apt update && apt install -y zip unzip && \
                cd /opt && wget https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip && \
                unzip sdk-tools-linux-4333796.zip && rm *.zip"

USER gitpod

RUN bash -c "source ~/.sdkman/bin/sdkman-init.sh && \
                sdk install java 8.0.232-open"