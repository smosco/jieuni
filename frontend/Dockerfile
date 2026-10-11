FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# next.config.ts의 rewrites 목적지. 빌드 결과물(routes-manifest)에 굳는다.
ARG API_URL=http://localhost:8080
ENV API_URL=$API_URL
# NEXT_PUBLIC_ 값은 빌드 때 브라우저 코드에 들어가므로 빌드 인자로 받는다.
ARG NEXT_PUBLIC_NEWSLETTER_URL=
ENV NEXT_PUBLIC_NEWSLETTER_URL=$NEXT_PUBLIC_NEWSLETTER_URL
ARG NEXT_PUBLIC_SITE_URL=
ENV NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL
# CSP img-src에 넣을 사진 주소. 헤더는 빌드 결과물에 굳는다.
ARG PHOTO_PUBLIC_BASE_URL=
ENV PHOTO_PUBLIC_BASE_URL=$PHOTO_PUBLIC_BASE_URL
RUN npm run build

FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production HOSTNAME=0.0.0.0 PORT=3000
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
USER node
EXPOSE 3000
CMD ["node", "server.js"]
