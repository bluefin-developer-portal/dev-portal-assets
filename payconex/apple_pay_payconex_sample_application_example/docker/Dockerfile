FROM php:8.3-apache

# curl + json are already enabled in the official image; enable rewrite/headers.
RUN a2enmod rewrite headers \
    && printf '%s\n' \
        'ServerName localhost' \
        '' \
        '<VirtualHost *:80>' \
        '    DocumentRoot /var/www/html/public' \
        '    <Directory /var/www/html/public>' \
        '        Options Indexes FollowSymLinks' \
        '        AllowOverride All' \
        '        Require all granted' \
        '    </Directory>' \
        '    <Directory "/var/www/html/public/.well-known">' \
        '        Options None' \
        '        Require all granted' \
        '        <Files "apple-developer-merchantid-domain-association">' \
        '            ForceType text/plain' \
        '        </Files>' \
        '    </Directory>' \
        '    ErrorLog ${APACHE_LOG_DIR}/error.log' \
        '    CustomLog ${APACHE_LOG_DIR}/access.log combined' \
        '</VirtualHost>' \
        > /etc/apache2/sites-available/000-default.conf

WORKDIR /var/www/html

# Build context is the repo root (see docker-compose.yml).
COPY . /var/www/html

# Boot with example config if a real config.php was not provided at build time.
RUN if [ ! -f /var/www/html/config.php ]; then cp /var/www/html/config.example.php /var/www/html/config.php; fi \
    && chown -R www-data:www-data /var/www/html

EXPOSE 80
