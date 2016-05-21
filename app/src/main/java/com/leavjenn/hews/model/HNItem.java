package com.leavjenn.hews.model;

public class HNItem {

    public static class Footer extends HNItem {

        public Footer() {
        }
    }

    public static class SearchResult extends HNItem {
        SearchHit[] hits;
        int nbHits;
        int page;
        int nbPages;
        int hitsPerPage;
        String query;

        public SearchResult() {
        }

        public void setHits(SearchHit[] hits) {
            this.hits = hits;
        }

        public SearchHit[] getHits(){
            return hits;
        }

        public int getNbPages(){
            return nbPages;
        }
    }

    public static class SearchHit extends HNItem {
        long objectID;

        public SearchHit() {
        }

        public long getObjectID() {
            return objectID;
        }
    }
}
