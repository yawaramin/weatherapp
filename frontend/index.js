const { h, app } = hyperapp;

const url_base = "localhost:8080";
const init_model = {
  forecasts: [], error: {}, select_city: {}, is_loading: false
};

function view(model, actions) {
  return h("ul", {},
    model.forecasts.map(f => h("li", {}, f)));
}

app({ model: init_model, view });
