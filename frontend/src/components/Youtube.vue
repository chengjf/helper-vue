<template>
  <div>
    <input type="text" v-model="source" placeholder="source">

    <button @click="submit()">Submit</button>

    <div>Result:
      <h2>linkUrl: {{ result.linkUrl }}</h2>

      <h2>title: {{ result.title }}</h2>
      <h2>sourceUrl: {{ result.sourceUrl }}</h2>
      <h2>sourceType: {{ result.sourceType }}</h2>
      <h2>format: {{ result.format }}</h2>
      <h2>duration: {{ result.duration }}</h2>
      <h2>size: {{ result.size }}</h2>
      <h2>coverPic: {{ result.coverPic }}</h2>
    </div>

  </div>
</template>

<script>
  import {AXIOS} from './http-common'

  export default {
    name: "Youtube",
    data() {
      return {
        errors: [],
        source: "",
        result: {}
      }
    },
    methods: {
      // Fetches posts when the component is created.
      submit() {
        console.log("source:", this.source)
        var params = new URLSearchParams()
        params.append('source', this.source)

        // var params = {
        //   "source": this.source
        // }

        AXIOS.post(`/youtube`, params)
          .then(response => {
            // JSON responses are automatically parsed.
            console.log(response.data)
            this.result = response.data
          })
          .catch(e => {
            this.errors.push(e)
          })
      }
    }
  }
</script>

<style scoped>

</style>
