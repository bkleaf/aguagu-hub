import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vuetify from 'vite-plugin-vuetify'
import path from 'path'

export default defineConfig({
  base: '/m/',
  plugins: [
    vue(),
    vuetify({ autoImport: true }),
  ],
  build: {
    // 모바일 전용 Spring Boot static 리소스 디렉토리로 빌드 결과물 출력
    outDir: path.resolve(__dirname, '../src/main/resources/static-mobile'),
    emptyOutDir: true,
  },
  server: {
    port: 3001,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
