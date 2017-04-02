const Forecast = {
  view: (model, actions) =>
    h("div.card", {},
      h("header.card-header", {},
        h("p.card-header-title", {}, Cities[model.id])),

      h("div.card-content", {},
        h("div.content", {}, model.days.map(day => day.dt))),

      h("footer.card-footer", {},
        h("a.card-footer-item", {}, "Save")))
}
