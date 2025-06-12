# Build stage
FROM node:16-alpine as builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install --production

# Runtime stage
FROM node:16-alpine

WORKDIR /app

# Copy built node modules and application code
COPY --from=builder /app/node_modules ./node_modules
COPY . .

# Expose the port the app runs on
EXPOSE 3000

# Run the application
CMD ["node", "server.js"]                                                                                                                                                                                     
