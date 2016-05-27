<?cs def:custom_masthead() ?>
    <!-- Header -->
    <div id="header">
        <div class="wrap" id="header-wrap">
            <div class="col-3 logo">
                <a href="<?cs var:toroot ?>reference/index.html">
                    <img src="<?cs var:toroot ?>assets/images/Recon-developers-icon.png" width="123" height="25" alt="Recon Developers" />
                </a>
            </div>
            <!-- New Search -->
            <div class="menu-container">
                <div class="search" id="search-container">
                    <div class="search-inner">
                        <div id="search-btn"></div>
                        <div class="left"></div>
                        <form onsubmit="return submit_search()">
                            <input id="search_autocomplete" type="text" value="" autocomplete="off" name="q"
                            onfocus="search_focus_changed(this, true)" onblur="search_focus_changed(this, false)"
                            onkeydown="return search_changed(event, true, '<?cs var:toroot ?>')" 
                            onkeyup="return search_changed(event, false, '<?cs var:toroot ?>')" />
                        </form>
                        <div class="right"></div>
                        <a class="close hide">close</a>
                        <div class="left"></div>
                        <div class="right"></div>
                    </div>
                </div>
                <div id="search_filtered_wrapper">
                    <div id="search_filtered_div" class="no-display">
                        <ul id="search_filtered">
                        </ul>
                    </div>
                </div>
            </div>
            <!-- /New Search>
            <!-- Expanded quicknav
            <div id="quicknav" class="col-9">
                <ul>
                    <li class="design">
                        <ul>
                            <li><a href="<?cs var:toroot ?>design/style/index.html">Style</a></li>
                            <li><a href="<?cs var:toroot ?>design/patterns/index.html">Patterns</a></li>
                            <li><a href="<?cs var:toroot ?>design/building-blocks/index.html">Building Blocks</a></li>
                            <li><a href="<?cs var:toroot ?>design/downloads/index.html">Downloads</a></li>
                        </ul>
                    </li>
                    <li class="develop">
                        <ul>
                            <li><a href="<?cs var:toroot ?>training/index.html">Android Training</a></li>
                            <li><a href="<?cs var:toroot ?>guide/components/index.html">API Guides</a></li>
                            <li><a href="<?cs var:toroot ?>reference/packages.html">Reference</a></li>
                            <li><a href="<?cs var:toroot ?>tools/index.html">Tools</a>
                                <ul><li><a href="<?cs var:toroot ?>sdk/index.html">Get the SDK</a></li></ul>
                            </li>
                        </ul>
                    </li>
                    <li class="distribute last">
                        <ul>
                            <li><a href="<?cs var:toroot ?>distribute/index.html">Google Play</a></li>
                            <li><a href="<?cs var:toroot ?>distribute/googleplay/publish/index.html">Publishing</a></li>
                            <li><a href="<?cs var:toroot ?>distribute/googleplay/promote/index.html">Promoting</a></li>
                            <li><a href="<?cs var:toroot ?>distribute/open.html">Open Distribution</a></li> 
                        </ul>
                    </li>
                </ul>
            </div>
            /Expanded quicknav -->
        </div>
    </div>
    <!-- /Header -->
    <div id="searchResults" class="wrap" style="display:none;">
        <h2 id="searchTitle">Results</h2>
        <div id="leftSearchControl" class="search-control">Loading...</div>
    </div>
<?cs /def ?>
