<template>
  <div>
    <input type="text" v-model="source" placeholder="source">

    <button @click="submit()">Submit</button>

    <div>Result:
      <textarea v-autosize v-model="result"></textarea>
    </div>

  </div>
</template>

<script>
  import {AXIOS} from './http-common'

  export default {
    name: "Json",
    data() {
      return {
        errors: [],
        source: "",
        result: ""
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

        AXIOS.post(`/json`, params)
          .then(response => {
            // JSON responses are automatically parsed.
            console.log(response.data)
            this.result = response.data.substring(1)
          })
          .catch(e => {
            this.errors.push(e)
          })
      }
    }
  }
</script>

<style scoped>
  textarea {
    width: 100%;
    white-space: pre;
    overflow: auto;
  }
</style>
