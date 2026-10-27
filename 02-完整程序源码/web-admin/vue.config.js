const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  // 生产构建不生成 sourcemap，减小产物体积、加快构建
  productionSourceMap: false,
  // 多核并行压缩（生产环境默认开启，显式声明更稳妥）
  parallel: true,
  devServer: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        onProxyRes(proxyRes) {
          // 强制给 JSON 响应头补上 charset=UTF-8，避免中文/泰语/越南语被浏览器按 Latin-1 解码成乱码
          const contentType = proxyRes.headers['content-type']
          if (contentType && contentType.includes('application/json') && !contentType.toLowerCase().includes('charset')) {
            proxyRes.headers['content-type'] = contentType + ';charset=UTF-8'
          }
        }
      }
    }
  },
  chainWebpack: (config) => {
    // 拆分 vendor chunk，提升缓存命中率与首屏加载速度
    config.optimization.splitChunks({
      chunks: 'all',
      cacheGroups: {
        echarts: {
          name: 'echarts',
          test: /[\\/]node_modules[\\/](echarts|zrender)[\\/]/,
          priority: 20,
          reuseExistingChunk: true
        },
        elementUI: {
          name: 'element-ui',
          test: /[\\/]node_modules[\\/]element-ui[\\/]/,
          priority: 15,
          reuseExistingChunk: true
        },
        vue: {
          name: 'vue',
          test: /[\\/]node_modules[\\/](vue|vue-router|vue-template-compiler)[\\/]/,
          priority: 10,
          reuseExistingChunk: true
        }
      }
    })
  }
})
