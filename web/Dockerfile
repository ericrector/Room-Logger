FROM php:7.1.2-apache 

MAINTAINER eric.m.rector@gmail.com


# Update and install requered packages
RUN set -x; \
    apt-get update \
	&& apt-get install -y --no-install-recommends \
        nano
	
RUN set -x; \
	docker-php-ext-install mysqli
	
COPY content /var/www/html/	