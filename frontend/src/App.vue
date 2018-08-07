<template>
  <div id="app">
    <Navigation></Navigation>
    <router-view :hellomsg="msg"></router-view>
  </div>
</template>

<script>
  import Navigation from './components/Navigation'


export default {
  name: 'app',
  components: {
    'Navigation': Navigation
  },
  data () {
    return {
      msg: 'Welcome to your Vue.js powered Spring Boot App'
    }
  }
}
</script>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 20px;
}
</style>
