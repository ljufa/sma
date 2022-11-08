use seed::{prelude::*, *};
use serde::{Deserialize, Serialize};

// ------ ------
//     Init
// ------ ------
#[cfg(debug_assertions)]
const API_BASE_URL: &str = "http://localhost:8080/";
#[cfg(not(debug_assertions))]
const API_BASE_URL: &str = "/";

const ROW_SIZE: usize = 4;
const DEFAULT_NUMBER_OF_TWEETS: usize = 12;
const DEFAULT_DAYS_IN_PAST: usize = 7;

fn init(url: Url, orders: &mut impl Orders<Msg>) -> Model {
    orders.perform_cmd(async { Msg::RulesFetched(get_matched_rules().await) });
    let rule_id = url.path().iter().last().cloned();
    Model {
        menu_visible: false,
        rules: vec![],
        selected_rule_id: rule_id,
        top_tweets: vec![],
        number_of_tweets: DEFAULT_NUMBER_OF_TWEETS,
        days_in_past: DEFAULT_DAYS_IN_PAST,
    }
}

// ------ ------
//     Model
// ------ ------
#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all(serialize = "snake_case", deserialize = "camelCase"))]
struct MatchedRule {
    id: String,
    tag: String,
    number_of_matches: u32,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all(serialize = "snake_case", deserialize = "camelCase"))]
struct TopTweet {
    tweet_id: String,
    number_of_refs: u32,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all(serialize = "camelCase", deserialize = "snake_case"))]
struct TopTweetsRequest {
    days_in_past: usize,
    limit: usize,
    rule_ids: Vec<String>,
}

struct Model {
    menu_visible: bool,
    rules: Vec<MatchedRule>,
    selected_rule_id: Option<String>,
    top_tweets: Vec<TopTweet>,
    number_of_tweets: usize,
    days_in_past: usize,
}

// ------ ------
//    Update
// ------ ------

enum Msg {
    RulesFetched(fetch::Result<Vec<MatchedRule>>),
    TopTweetsFetched(fetch::Result<Vec<TopTweet>>),
    RuleSelected(String),
    ToggleMenu,
    HideMenu,
}

fn update(msg: Msg, model: &mut Model, orders: &mut impl Orders<Msg>) {
    match msg {
        Msg::ToggleMenu => model.menu_visible = not(model.menu_visible),
        Msg::HideMenu => {
            if model.menu_visible {
                model.menu_visible = false;
            } else {
                orders.skip();
            }
        }
        Msg::RulesFetched(Ok(rules)) => {
            if !rules.is_empty() {
                model.rules = rules;
                let req = TopTweetsRequest {
                    limit: model.number_of_tweets,
                    days_in_past: model.days_in_past,
                    rule_ids: model
                        .selected_rule_id
                        .as_ref()
                        .map_or(vec![], |sr| vec![sr.clone()]),
                };
                orders.perform_cmd(async { Msg::TopTweetsFetched(get_top_tweets(req).await) });
            }
        }
        Msg::RulesFetched(Err(err)) => {
            log!("Error fetching rules", err);
        }
        Msg::TopTweetsFetched(Ok(tweets)) => {
            model.top_tweets = tweets;
        }
        Msg::TopTweetsFetched(Err(err)) => {
            log!("Error fetching toptweets", err);
        }
        Msg::RuleSelected(sel_rule) => model.selected_rule_id = Some(sel_rule),
    }
}

async fn get_top_tweets(request: TopTweetsRequest) -> fetch::Result<Vec<TopTweet>> {
    Request::new(format!("{}api/tw/top", API_BASE_URL))
        .method(Method::Post)
        .json(&request)?
        .fetch()
        .await?
        .check_status()?
        .json::<Vec<TopTweet>>()
        .await
}

async fn get_matched_rules() -> fetch::Result<Vec<MatchedRule>> {
    Request::new(format!("{}api/tw/matchedrules", API_BASE_URL))
        .method(Method::Get)
        .fetch()
        .await?
        .check_status()?
        .json::<Vec<MatchedRule>>()
        .await
}

// ------ ------
//     View
// ------ ------

fn view(model: &Model) -> Vec<Node<Msg>> {
    vec![view_navbar(model), view_content(model)]
}

// ----- view_content ------

fn view_content(model: &Model) -> Node<Msg> {
    div![
        C!["tile", "is-ancestor", "is-vertical"],
        model.top_tweets.chunks(ROW_SIZE).map(|row| {
            div![
                C!["tile", "is-parent"],
                row.iter().map(|tw| div![
                    C!["tile","is-child" "box"],
                    blockquote![
                        C!["twitter-tweet"],
                        raw!(
                            r#"
                        <div class="card">
                            <div class="card-image">
                                <div class="load-wraper">
                                    <div class="activity"></div>
                                </div>
                            </div>
                        </div>"#
                        ),
                        // a!( format!("https://twitter.com/web/status/{}", tw.tweet_id))
                        a!(attrs! {
                            At::Href => format!("https://twitter.com/web/status/{}", tw.tweet_id)
                        })
                    ]
                ])
            ]
        })
    ]
}

// ----- view_navbar ------

fn view_navbar(model: &Model) -> Node<Msg> {
    nav![
        C!["navbar", "is-link"],
        attrs! {
            At::from("role") => "navigation",
            At::AriaLabel => "main navigation",
        },
        div![
            C!["navbar-brand"],
            // ------ Logo ------
            a![
                C!["navbar-item", "has-text-weight-bold", "is-size-3"],
                "SMA - FOR DEMO PURPOSE ONLY!"
            ],
            // ------ Hamburger ------
            a![
                C![
                    "navbar-burger",
                    "burger",
                    IF!(model.menu_visible => "is-active")
                ],
                style! {
                    St::MarginTop => "auto",
                    St::MarginBottom => "auto",
                },
                attrs! {
                    At::from("role") => "button",
                    At::AriaLabel => "menu",
                    At::AriaExpanded => model.menu_visible,
                },
                ev(Ev::Click, |event| {
                    event.stop_propagation();
                    Msg::ToggleMenu
                }),
                span![attrs! {At::AriaHidden => "true"}],
                span![attrs! {At::AriaHidden => "true"}],
                span![attrs! {At::AriaHidden => "true"}],
            ]
        ],
        div![
            C!["navbar-menu", IF!(model.menu_visible => "is-active")],
            div![C!["navbar-start"],],
            div![
                C!["navbar-end"],
                div![
                    C!["navbar-item"],
                    div![
                        C!["buttons"],
                        model.rules.iter().map(|rule| {
                            a![
                                C!["button", IF!(model.selected_rule_id.as_ref().map_or(false, |f| f == &rule.id) => "is-primary")],
                                attrs![
                                    At::Href => rule.id,
                                ],
                                strong![format!("{} ({})", &rule.tag, rule.number_of_matches)],
                            ]
                        })
                    ]
                ]
            ]
        ]
    ]
}

// ------ ------
//     Start
// ------ ------

#[wasm_bindgen(start)]
pub fn start() {
    App::start("app", init, update, view);
}
