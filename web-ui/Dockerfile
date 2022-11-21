FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/
COPY style.css /usr/share/nginx/html/
COPY index.js /usr/share/nginx/html/
COPY custom.js /usr/share/nginx/html/
RUN mkdir /usr/share/nginx/html/pkg
COPY pkg /usr/share/nginx/html/pkg
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]