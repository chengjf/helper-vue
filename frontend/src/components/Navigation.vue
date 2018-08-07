<template>
  <div>
    <h2>Nav Bar</h2>
    <nav>
      <router-link to="/">Home</router-link>|
      <router-link to="/md5">MD5</router-link>|
      <router-link to="/json">JSON</router-link>|
      <router-link to="/youtube">Youtube</router-link>
    </nav>
  </div>
</template>

<script>
    export default {
        name: "Navigation"
    }
</script>

<style scoped>

</style>
