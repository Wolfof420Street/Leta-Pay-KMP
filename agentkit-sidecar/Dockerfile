FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx tsc -p tsconfig.json

FROM node:22-alpine AS runtime
WORKDIR /app
RUN addgroup -S letapay && adduser -S letapay -G letapay
USER letapay
COPY --from=build /app/dist ./dist
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package.json ./package.json
EXPOSE 3100
CMD ["node", "dist/index.js"]
